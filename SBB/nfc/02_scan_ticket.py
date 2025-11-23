# ...existing code...
#!/usr/bin/env python3
"""Simulate a ticket inspector: read a ticket JSON and report if it's valid for a train.

Usage:
python3 02_scan_ticket.py --ticket tickets/72cb0dbb-50eb-4bf5-ab20-6ed14be39035-geneva-biel.json --train ic1
python3 02_scan_ticket.py --ticket tickets/72cb0dbb-50eb-4bf5-ab20-6ed14be39035-geneva-biel.json --train ic5

./UserA/private_key.pem
./UserA/public_key.pem
./UserB/private_key.pem
./UserB/public_key.pem
"""
import argparse
import json
import sys
import csv
import base64
from pathlib import Path

# cryptography used to check keys match
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

    
def validate_ticket(ticket: dict, train) -> bool:
    # Simple validator: always returns True (ticket is valid for the requested train).
    return True


def load_pub_pem(path: str) -> bytes:
    p = Path(path)
    data = p.read_bytes()
    # accept raw PEM or DER; assume PEM given by user
    return data


def load_priv_and_derive_pub(priv_path: str) -> bytes:
    p = Path(priv_path)
    priv_pem = p.read_bytes()
    private_key = serialization.load_pem_private_key(priv_pem, password=None, backend=default_backend())
    pub = private_key.public_key()
    pub_pem = pub.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return pub_pem


def find_ticket_entry(db_path: Path, ticket_id: str):
    """Return (train, pubkey_pem_bytes) if ticket found, else None."""
    if not db_path.exists():
        return None
    try:
        with db_path.open("r", encoding="utf-8", newline="") as fh:
            reader = csv.reader(fh)
            for row in reader:
                if not row:
                    continue
                # expected: ticket_id,train,pubkey_b64
                if row[0] == ticket_id:
                    train = row[1] if len(row) > 1 else ""
                    pub_b64 = row[2] if len(row) > 2 else ""
                    try:
                        pub_pem = base64.b64decode(pub_b64) if pub_b64 else None
                    except Exception:
                        pub_pem = None
                    return train, pub_pem
    except Exception:
        # fallback: raw search
        content = db_path.read_text(encoding="utf-8")
        if ticket_id in content:
            return ("", None)
    return None


def append_ticket_entry(db_path: Path, ticket_id: str, train, pub_pem) -> None:
    if db_path.parent and not db_path.parent.exists():
        db_path.parent.mkdir(parents=True, exist_ok=True)
    write_header = not db_path.exists()
    with db_path.open("a", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        if write_header:
            writer.writerow(["ticket_id", "train", "pubkey_b64"])
        pub_b64 = base64.b64encode(pub_pem).decode("ascii") if pub_pem else ""
        writer.writerow([ticket_id, train or "", pub_b64])


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulate ticket inspection")
    parser.add_argument("--ticket", help="Path to ticket JSON", required=True)
    parser.add_argument("--train", help="Line/train to check (optional)", required=True)
    parser.add_argument("--pubkey", help="(optional) path to public key to register when ticket first scanned")
    parser.add_argument("--privkey", help="(optional) path to private key to prove ownership when ticket already registered")
    args = parser.parse_args()

    ticket_path = Path(args.ticket)
    if not ticket_path.exists():
        print(f"ERROR: ticket file not found: {ticket_path}")
        sys.exit(2)

    try:
        ticket = json.loads(ticket_path.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"ERROR: failed to read/parse ticket JSON: {exc}")
        sys.exit(2)

    required_keys = {"id", "departure-station", "arrival-station", "day-of-travel"}
    missing = required_keys - ticket.keys()
    if missing:
        print("ERROR: ticket JSON is missing required fields.")
        print(f"  Missing: {', '.join(sorted(missing))}")
        print(f"  Present keys: {', '.join(sorted(ticket.keys()))}")
        sys.exit(2)

    ok = validate_ticket(ticket, args.train)
    ticket_id = ticket["id"]

    db_name = f"sbb-database/{args.train}-db.txt"
    db_path = Path(db_name)

    entry = find_ticket_entry(db_path, ticket_id)

    if entry:
        # ticket already registered: verify user has corresponding private key
        _, registered_pub_pem = entry
        if not registered_pub_pem:
            print(f"ERROR: ticket {ticket_id} found but no pubkey stored. Cannot verify ownership.")
            sys.exit(1)

        priv_path = args.privkey or input(
            "\033[93mTicket already registered\033[0m — enter path to your private key file (e.g. ./private_key.pem): "
        ).strip()
        try:
            derived_pub = load_priv_and_derive_pub(priv_path)
        except Exception as exc:
            print(f"ERROR: failed to load/derive public from private key: {exc}")
            sys.exit(2)

        if derived_pub == registered_pub_pem:
            print("\033[92mTICKET VALID & OWNER VERIFIED\033[0m")
            print(f"  id:       {ticket_id}")
            print(f"  database: {db_path}")
            sys.exit(0)
        else:
            print("\033[91mTICKET INVALID\033[0m: provided private key does not match stored public key")
            sys.exit(1)

    else:
        # first time scan: register pubkey (ask user)
        pub_path = args.pubkey or input(
            "\033[93mTicket not registered\033[0m — enter path to the public key file to register (e.g. ./public_key.pem): "
        ).strip()
        try:
            pub_pem = load_pub_pem(pub_path)
        except Exception as exc:
            print(f"ERROR: failed to load public key: {exc}")
            sys.exit(2)

        # store entry
        append_ticket_entry(db_path, ticket_id, args.train, pub_pem)
        print("\033[92mTICKET VALID — Registered\033[0m")
        print(f"  id:        {ticket_id}")
        print(f"  departure: {ticket['departure-station']}")
        print(f"  arrival:   {ticket['arrival-station']}")
        print(f"  day:       {ticket['day-of-travel']}")
        print(f"  train:     {args.train}")
        print(f"  registered public key saved in: {db_path}")
        sys.exit(0)


if __name__ == "__main__":
    main()