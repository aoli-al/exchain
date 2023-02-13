from enum import Enum


class Exception:
    def __init__(self, type: str, method: str, message: str) -> None:
        self.type = type
        self.method = method
        self.message = message

    def __eq__(self, __o: object) -> bool:
        if not isinstance(__o, Exception):
            return False
        return __o.method == self.method and __o.type == self.type


    def __hash__(self) -> int:
        return hash((self.type, self.method))

    def __str__(self) -> str:
        return f"{self.type}:{self.message}:{self.method}"

class LinkType(Enum):
    IGNORE = 1
    OPTIONAL = 2
    KEY = 3


class Link:
    def __init__(self, src: Exception, dst: Exception) -> None:
        self.src = src
        self.dst = dst

    def __eq__(self, __o: object) -> bool:
        if not isinstance(__o, Link):
            return False
        return __o.src == self.src and __o.dst == self.dst

    def __hash__(self) -> int:
        return hash((self.src, self.dst))

    def __str__(self) -> str:
        return str(self.src) + " -> " + str(self.dst) + "\n"
