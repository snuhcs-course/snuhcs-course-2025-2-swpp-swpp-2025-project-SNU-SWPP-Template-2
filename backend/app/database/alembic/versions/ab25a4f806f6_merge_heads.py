"""Merge heads

Revision ID: ab25a4f806f6
Revises: 02c823219282, 507091d6e5c7
Create Date: 2025-11-06 19:16:27.477155

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'ab25a4f806f6'
down_revision: Union[str, Sequence[str], None] = ('02c823219282', '507091d6e5c7')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
