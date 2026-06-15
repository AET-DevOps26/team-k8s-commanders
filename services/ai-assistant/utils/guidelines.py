"""Clinical-guidelines retrieval — the RAG path of the assistant.

This is deliberately *separate* from patient grounding (``utils.context``):
patient facts are authoritative and injected directly, whereas general medical
guidelines are retrieved semantically from a pgvector collection living in the
same ai-db Postgres. Seed the collection with ``scripts/ingest_guidelines.py``.

Retrieval is best-effort. If the collection was never ingested, the pgvector
extension is missing, or the embedding backend is unreachable, retrieval logs a
warning and returns ``[]`` instead of raising — the patient-grounding path must
never be blocked by the knowledge base being absent.

Each hit becomes a LangChain ``Document`` whose ``metadata["source"]`` reads
``Clinical guideline: <title>`` so it surfaces in ``AIMessageResponse.sources``
just like the patient/appointment/note documents do.
"""

import logging
import os
from functools import lru_cache

from langchain_core.documents import Document
from langchain_postgres import PGVector

from db.engine import database_url
from utils.embeddings import get_embeddings

logger = logging.getLogger(__name__)

COLLECTION_NAME = "clinical_guidelines"
SOURCE_PREFIX = "Clinical guideline"

# Retrieval knobs, overridable via env so they can be tuned without a redeploy.
DEFAULT_K = int(os.getenv("GUIDELINES_TOP_K", "4"))
# Cosine distance cutoff (0 = identical, 2 = opposite). Hits beyond this are
# dropped so an off-topic question pulls in no guideline noise.
DEFAULT_MAX_DISTANCE = float(os.getenv("GUIDELINES_MAX_DISTANCE", "0.55"))


@lru_cache(maxsize=2)
def get_vector_store(create_extension: bool = False) -> PGVector:
    """Return a (cached) PGVector store over the ai-db collection.

    ``create_extension`` is left off on the read path — the ingestion script
    owns the one-time ``CREATE EXTENSION vector`` — so a query never issues DDL.
    The embedding dimension is taken from ``EMBEDDING_DIM`` when set; pgvector
    needs it to create the column, but it is unused once the table exists.
    """
    embedding_length = os.getenv("EMBEDDING_DIM")
    return PGVector(
        embeddings=get_embeddings(),
        collection_name=COLLECTION_NAME,
        connection=database_url(),
        embedding_length=int(embedding_length) if embedding_length else None,
        use_jsonb=True,
        create_extension=create_extension,
        async_mode=True,
    )


def _to_document(doc: Document) -> Document:
    """Relabel a retrieved chunk's source to ``Clinical guideline: <title>``."""
    title = (doc.metadata or {}).get("title") or "untitled"
    return Document(
        page_content=doc.page_content,
        metadata={**(doc.metadata or {}), "source": f"{SOURCE_PREFIX}: {title}"},
    )


async def retrieve_guidelines(
    query: str,
    k: int = DEFAULT_K,
    max_distance: float = DEFAULT_MAX_DISTANCE,
) -> list[Document]:
    """Return up to ``k`` guideline chunks relevant to ``query``.

    Returns ``[]`` on any retrieval failure (store not ready, embeddings down),
    so callers can treat the knowledge base as an optional enrichment.
    """
    query = (query or "").strip()
    if not query:
        return []

    try:
        store = get_vector_store()
        hits = await store.asimilarity_search_with_score(query, k=k)
    except Exception as e:  # noqa: BLE001 — degrade gracefully, never block the reply
        logger.warning("Guideline retrieval unavailable, continuing without it: %s", e)
        return []

    return [_to_document(doc) for doc, distance in hits if distance <= max_distance]
