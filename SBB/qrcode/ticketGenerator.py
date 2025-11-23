from dataclasses import dataclass, asdict
from datetime import datetime
from uuid import uuid4
import json
import argparse
import qrcode
from qrcode.constants import ERROR_CORRECT_Q
from typing import Dict, Optional

#!/usr/bin/env python3
"""
ticketGenerator.py

Simple QR ticket generator.

Dependencies:
    pip install qrcode[pil]

Usage example:
    python ticketGenerator.py --from "Zurich" --to "Geneva" --travel-date 2025-12-24 \
        --ticket-type normal --train-id IC200 --out ticket.png
"""


VALID_TICKET_TYPES = {"normal", "special"}


@dataclass
class Ticket:
    origin: str
    destination: str
    travel_date: str 
    ticket_type: str 
    train_id: Optional[str] = None
    ticket_id: str = None
    issued_at: str = None

    def __post_init__(self):
        if self.ticket_type not in VALID_TICKET_TYPES:
            raise ValueError(f"ticket_type must be one of {VALID_TICKET_TYPES}")
        if self.ticket_type == "special" and not self.train_id:
            raise ValueError("train_id is required for special tickets")
        if self.ticket_id is None:
            self.ticket_id = uuid4().hex
        if self.issued_at is None:
            self.issued_at = datetime.utcnow().isoformat() + "Z"

    def to_payload(self) -> Dict:
        payload = {
            "from": self.origin,
            "to": self.destination,
            "travelDate": self.travel_date,
            "ticketType": self.ticket_type,
            "ticketId": self.ticket_id,
            "issuedAt": self.issued_at,
        }
        if self.train_id is not None:
            payload["trainId"] = self.train_id
        return payload

    def to_json(self) -> str:
        return json.dumps(self.to_payload(), separators=(",", ":"), ensure_ascii=False)


def generate_qr_from_ticket(ticket: Ticket, out_path: str, box_size: int = 10, border: int = 4):
    """
    Generate a PNG QR code file from a Ticket instance.
    """
    payload = ticket.to_json()
    qr = qrcode.QRCode(error_correction=ERROR_CORRECT_Q, box_size=box_size, border=border)
    qr.add_data(payload)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    img.save(out_path)
    return out_path


def parse_args():
    p = argparse.ArgumentParser(description="Generate a ticket QR code PNG.")
    p.add_argument("--from", dest="from_", required=True, help="Origin/station")
    p.add_argument("--to", required=True, help="Destination/station")
    p.add_argument("--travel-date", required=True, help="Travel date (YYYY-MM-DD)")
    p.add_argument("--ticket-type", default="normal", choices=list(VALID_TICKET_TYPES), help="Ticket type")
    p.add_argument("--train-id", required=False, help="Train identifier (required when ticket-type is 'special')")
    p.add_argument("--out", default="ticket.png", help="Output PNG file path")
    return p.parse_args()


def main():
    args = parse_args()
    if args.ticket_type == "special" and not args.train_id:
        raise SystemExit("train-id is required when ticket-type is 'special'")

    try:
        _ = datetime.fromisoformat(args.travel_date)
    except Exception:
        try:
            datetime.strptime(args.travel_date, "%Y-%m-%d")
        except Exception:
            raise SystemExit("travel-date must be ISO format (YYYY-MM-DD or full ISO timestamp)")

    ticket = Ticket(
        origin=args.from_,
        destination=args.to,
        travel_date=args.travel_date,
        ticket_type=args.ticket_type,
        train_id=args.train_id,
    )
    out = generate_qr_from_ticket(ticket, args.out)
    print(out)


if __name__ == "__main__":
    main()