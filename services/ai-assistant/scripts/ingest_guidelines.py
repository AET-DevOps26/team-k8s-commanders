#!/usr/bin/env python
"""Ingest the curated clinical-guideline corpus into the pgvector collection.

Reads every ``*.md`` under ``data/guidelines/``, splits each into chunks, embeds
them with the configured embedding model, and writes them to the
``clinical_guidelines`` pgvector collection in the ai-db Postgres. The assistant
then retrieves from this collection at query time (see ``utils.guidelines``).

The run is idempotent: it rebuilds the collection from scratch each time
(``pre_delete_collection``), so editing or removing a guideline file is fully
reflected on the next run without leaving stale chunks behind.

Usage (from services/ai-assistant, with the venv active and DB_* / DATABASE_URL
and the embedding env set):

    python scripts/ingest_guidelines.py
    python scripts/ingest_guidelines.py --dir data/guidelines

Run it once after the stack is up, or as a one-shot Kubernetes Job in deploy.
"""

import argparse
import asyncio
import logging
import sys
from pathlib import Path

from langchain_core.documents import Document
from langchain_postgres import PGVector
from langchain_text_splitters import RecursiveCharacterTextSplitter

# Allow `python scripts/ingest_guidelines.py` from the service root by putting
# that root (which holds the db/ and utils/ packages) on the import path.
SERVICE_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SERVICE_ROOT))

from db.engine import database_url  # noqa: E402
from utils.embeddings import get_embeddings  # noqa: E402
from utils.guidelines import COLLECTION_NAME  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
logger = logging.getLogger("ingest_guidelines")

DEFAULT_DIR = SERVICE_ROOT / "data" / "guidelines"


def _parse(path: Path) -> tuple[str, str]:
    """Return ``(title, body)`` for a guideline file.

    Supports an optional ``---\\ntitle: ...\\n---`` frontmatter block; otherwise
    falls back to the first ``# `` heading, then the file stem. Parsed by hand to
    avoid pulling in a YAML dependency for one field.
    """
    text = path.read_text(encoding="utf-8")
    title = None
    body = text

    if text.startswith("---"):
        end = text.find("\n---", 3)
        if end != -1:
            front = text[3:end]
            body = text[end + 4 :].lstrip("\n")
            for line in front.splitlines():
                if line.strip().lower().startswith("title:"):
                    title = line.split(":", 1)[1].strip()
                    break

    if not title:
        for line in body.splitlines():
            if line.startswith("# "):
                title = line[2:].strip()
                break

    return (title or path.stem.replace("-", " ").title()), body


def _load_chunks(guidelines_dir: Path) -> list[Document]:
    files = sorted(guidelines_dir.glob("*.md"))
    if not files:
        raise SystemExit(f"No guideline files (*.md) found in {guidelines_dir}")

    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=150)
    docs: list[Document] = []
    for path in files:
        title, body = _parse(path)
        for i, chunk in enumerate(splitter.split_text(body)):
            docs.append(
                Document(
                    page_content=chunk,
                    metadata={"title": title, "source_file": path.name, "chunk": i},
                )
            )
        logger.info("Loaded %s ('%s')", path.name, title)
    return docs


async def ingest(guidelines_dir: Path) -> None:
    docs = _load_chunks(guidelines_dir)

    store = PGVector(
        embeddings=get_embeddings(),
        collection_name=COLLECTION_NAME,
        connection=database_url(),
        use_jsonb=True,
        # Ensure the extension exists and start from a clean collection so the
        # run is idempotent and reflects deletions/edits to the corpus.
        create_extension=True,
        pre_delete_collection=True,
        async_mode=True,
    )
    await store.aadd_documents(docs)
    logger.info(
        "Ingested %d chunks into collection '%s'", len(docs), COLLECTION_NAME
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dir",
        type=Path,
        default=DEFAULT_DIR,
        help=f"Directory of guideline *.md files (default: {DEFAULT_DIR})",
    )
    args = parser.parse_args()
    asyncio.run(ingest(args.dir.resolve()))


if __name__ == "__main__":
    main()
