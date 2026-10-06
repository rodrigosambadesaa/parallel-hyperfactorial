#!/usr/bin/env python3
"""Certified complex hyperfactorial worker using FLINT/Arb."""

from __future__ import annotations

import hashlib
import os
import sys
import time
from decimal import Decimal

try:
    from flint import acb, arb, ctx, fmpq
except ImportError as error:
    raise SystemExit(
        "python-flint is required. Install it with: "
        "python3 -m pip install -r requirements.txt"
    ) from error


GUARD_DIGITS = 64


def decimal_rational(text: str) -> fmpq:
    value = Decimal(text)
    if not value.is_finite():
        raise ValueError("NaN and infinity are not valid inputs")
    sign, digits, exponent = value.as_tuple()
    numerator = int("".join(str(digit) for digit in digits) or "0")
    if sign:
        numerator = -numerator
    if exponent >= 0:
        return fmpq(numerator * (10 ** exponent), 1)
    return fmpq(numerator, 10 ** (-exponent))


def stable_decimal(ball: arb, digits: int) -> str:
    lower = ball.lower().str(digits, radius=False)
    upper = ball.upper().str(digits, radius=False)
    if lower != upper:
        raise ArithmeticError(
            "The interval does not determine a unique decimal rounding; "
            "increase GUARD_DIGITS"
        )
    plain = format(Decimal(lower), "f")
    return "0" if Decimal(plain).is_zero() else plain


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit("usage: worker REAL IMAGINARY PRECISION_DIGITS")
    if hasattr(sys, "set_int_max_str_digits"):
        sys.set_int_max_str_digits(0)

    real_text, imaginary_text, precision_text = sys.argv[1:]
    precision_digits = int(precision_text)
    if precision_digits <= 0:
        raise ValueError("precision must be positive")

    working_digits = precision_digits + GUARD_DIGITS
    ctx.dps = working_digits
    ctx.threads = max(1, int(os.environ.get(
        "HYPERFACTORIAL_THREADS", min(8, os.cpu_count() or 1))))

    argument = acb(
        arb(decimal_rational(real_text)),
        arb(decimal_rational(imaginary_text)),
    )
    shifted = argument + 1

    started = time.perf_counter()
    # Principal analytic continuation:
    # H(z) = exp(z log(Gamma(z+1)) - log(G(z+1))).
    value = (argument * shifted.lgamma() - shifted.log_barnes_g()).exp()
    elapsed = time.perf_counter() - started

    real_result = stable_decimal(value.real, precision_digits)
    imaginary_result = stable_decimal(value.imag, precision_digits)
    canonical = f"{real_result}\n{imaginary_result}\n".encode("ascii")

    fields = {
        "CERTIFIED": "true",
        "PRECISION_DIGITS": precision_digits,
        "WORKING_DIGITS": working_digits,
        "REAL_ACCURACY_BITS": value.real.rel_accuracy_bits(),
        "IMAGINARY_ACCURACY_BITS": value.imag.rel_accuracy_bits(),
        "TIME_SECONDS": f"{elapsed:.9f}",
        "SHA256": hashlib.sha256(canonical).hexdigest(),
        "REAL": real_result,
        "IMAGINARY": imaginary_result,
    }
    for name, field_value in fields.items():
        print(f"{name}\t{field_value}")


if __name__ == "__main__":
    main()
