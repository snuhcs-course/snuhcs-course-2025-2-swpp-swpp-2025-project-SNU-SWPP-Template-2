import abc

class Factory(abc.ABC):
    @abc.abstractmethod
    def create(self, key_type: str):
        pass