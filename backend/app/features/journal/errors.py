from fastapi import HTTPException


class JournalNotFoundError(HTTPException):
    def __init__(self, journal_id: int) -> None:
        super().__init__(
            status_code=404, detail=f"Journal with ID {journal_id} not found"
        )


class UnauthorizedAccessError(HTTPException):
    def __init__(self) -> None:
        super().__init__(
            status_code=403, detail="You do not have permission to access this journal"
        )


class JournalUpdateError(HTTPException):
    def __init__(self) -> None:
        super().__init__(
            status_code=400, detail="At least one field must be provided for update"
        )


class JournalBadRequestError(HTTPException):
    def __init__(self, detail: str) -> None:
        super().__init__(
            status_code=400,
            detail="invalid request: " + detail,
        )


class ImageUploadError(HTTPException):
    def __init__(self, detail: str) -> None:
        super().__init__(
            status_code=500,
            detail="image upload error: " + detail,
        )
