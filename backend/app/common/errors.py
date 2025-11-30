from fastapi import HTTPException


class InvalidFieldFormatError(HTTPException):
    def __init__(self, field_name: str):
        super().__init__(
            status_code=400, detail=f"Invalid format for field {field_name}"
        )


class PermissionDeniedError(HTTPException):
    def __init__(self) -> None:
        super().__init__(status_code=401, detail="Permission denied")
