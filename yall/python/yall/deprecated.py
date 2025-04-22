import functools
import warnings
from typing import Callable, ParamSpec, TypeVar

rT = TypeVar("rT")  # return type
pT = ParamSpec("pT")  # parameters type


def deprecated(func: Callable[pT, rT]) -> Callable[pT, rT]:
    """Use this decorator to mark functions as deprecated.
    Every time the decorated function runs, it will emit
    a "deprecation" warning."""

    @functools.wraps(func)
    def new_func(*args: pT.args, **kwargs: pT.kwargs):
        warnings.simplefilter("always", DeprecationWarning)  # turn off filter
        warnings.warn(
            "Call to a deprecated function {}.".format(func.__name__),
            category=DeprecationWarning,
            stacklevel=2,
        )
        warnings.simplefilter("default", DeprecationWarning)  # reset filter
        return func(*args, **kwargs)

    return new_func
