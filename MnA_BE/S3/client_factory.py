import boto3

from S3 import _get_env
from utils.factory import Factory

class S3ClientFactory(Factory):
    """
    S3 Client 생성을 담당하는 Factory.

    - from_env: 기본(공통) S3 클라이언트
    - for_finance: Finance 전용 S3 클라이언트
    """

    def create(self, key_type: str = "default"):
        if key_type == "finance":
            return boto3.client(
                "s3",
                aws_access_key_id=_get_env("FINANCE_IAM_ACCESS_KEY_ID", "FINANCE_AWS_ACCESS_KEY_ID"),
                aws_secret_access_key=_get_env("FINANCE_IAM_SECRET_KEY", "FINANCE_AWS_SECRET_ACCESS_KEY"),
                region_name=_get_env("FINANCE_AWS_REGION", "AWS_REGION"),
            )

        # default client
        return boto3.client(
            "s3",
            aws_access_key_id=_get_env("IAM_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=_get_env("IAM_SECRET_KEY", "AWS_SECRET_ACCESS_KEY"),
            region_name=_get_env("AWS_REGION")
        )
