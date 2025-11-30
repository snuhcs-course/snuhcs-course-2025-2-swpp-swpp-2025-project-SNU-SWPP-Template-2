"""merge heads

Revision ID: ae3935ab297a
Revises: 02c823219282, 507091d6e5c7
Create Date: 2025-11-04 15:01:21.275480

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'ae3935ab297a'
down_revision: Union[str, Sequence[str], None] = ('02c823219282', '507091d6e5c7')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
