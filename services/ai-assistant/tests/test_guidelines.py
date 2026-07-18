"""Tests for the clinical-guidelines RAG retrieval path (utils/guidelines.py).

The pgvector store is never stood up here; ``get_vector_store`` is patched with a
fake so the filtering/labelling logic is exercised in isolation, and the
graceful-degradation path is asserted directly. Async helpers are driven with
``asyncio.run`` to match the project's existing async tests (no pytest-asyncio).
"""

import asyncio
from unittest.mock import patch

from langchain_core.documents import Document

from utils import guidelines


class _FakeStore:
    """Stand-in for PGVector returning canned (Document, distance) pairs."""

    def __init__(self, hits):
        self._hits = hits

    async def asimilarity_search_with_score(self, query, k):
        return self._hits[:k]


def _hit(title: str, distance: float, text: str = "body") -> tuple[Document, float]:
    return Document(page_content=text, metadata={"title": title}), distance


def test_retrieve_relabels_source_with_title():
    store = _FakeStore([_hit("Hypertension", 0.1)])
    with patch.object(guidelines, "get_vector_store", return_value=store):
        docs = asyncio.run(guidelines.retrieve_guidelines("blood pressure"))

    assert len(docs) == 1
    assert docs[0].metadata["source"] == "Clinical guideline: Hypertension"
    # Original metadata is preserved alongside the new source label.
    assert docs[0].metadata["title"] == "Hypertension"


def test_retrieve_drops_hits_beyond_distance_threshold():
    store = _FakeStore([_hit("Close", 0.2), _hit("Far", 0.9)])
    with patch.object(guidelines, "get_vector_store", return_value=store):
        docs = asyncio.run(guidelines.retrieve_guidelines("q", max_distance=0.55))

    assert [d.metadata["title"] for d in docs] == ["Close"]


def test_retrieve_untitled_hit_falls_back():
    doc = Document(page_content="x", metadata={})
    store = _FakeStore([(doc, 0.1)])
    with patch.object(guidelines, "get_vector_store", return_value=store):
        docs = asyncio.run(guidelines.retrieve_guidelines("q"))

    assert docs[0].metadata["source"] == "Clinical guideline: untitled"


def test_retrieve_empty_query_returns_nothing_without_touching_store():
    with patch.object(guidelines, "get_vector_store") as mock_store:
        assert asyncio.run(guidelines.retrieve_guidelines("   ")) == []
    mock_store.assert_not_called()


def test_retrieve_degrades_to_empty_on_failure():
    """A store/embedding failure must never propagate — retrieval is best-effort."""
    with patch.object(
        guidelines, "get_vector_store", side_effect=RuntimeError("pgvector down")
    ):
        assert asyncio.run(guidelines.retrieve_guidelines("anything")) == []
