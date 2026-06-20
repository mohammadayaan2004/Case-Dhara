"""
Global SlowAPI rate limiter for Case Dhara (IP-based).
"""

from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
