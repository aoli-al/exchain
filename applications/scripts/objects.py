class Exception:
    def __init__(self, type: str, message: str) -> None:
        self.type = type
        self.message = message

    def __eq__(self, __o: object) -> bool:
        if not isinstance(__o, Exception):
            return False
        return __o.message == self.message and __o.type == self.type

    def __hash__(self) -> int:
        return hash((self.type, self.message))

    def __str__(self) -> str:
        return f"{self.type}:{self.message}"

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