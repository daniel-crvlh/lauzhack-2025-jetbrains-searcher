#!/usr/bin/env python3
"""Simple script to write 2 or 3 positional args into a JSON file.

Usage:
  python3 01_buy_ticket.py --departure-station geneva --arrival-station biel --day-of-travel 2025-11-22
"""
import argparse
import json
import uuid
from pathlib import Path


def build_parser():
    p = argparse.ArgumentParser(description="Write provided args to a JSON file")
    p.add_argument("--departure-station", required=True,
                   help="Departure station (e.g. geneva)")
    p.add_argument("--arrival-station", required=True,
                   help="Arrival station (e.g. biel)")
    p.add_argument("--day-of-travel", required=True,
                   help="Day of travel (YYYY-MM-DD)")
    return p


def main():
    parser = build_parser()
    args = parser.parse_args()

    data = {
        "id": str(uuid.uuid4()),
        "departure-station": args.departure_station,
        "arrival-station": args.arrival_station,
        "day-of-travel": args.day_of_travel,
    }


    out_path = f"tickets/{data['id']}-{data['departure-station']}-{data['arrival-station']}.json"
    out_file = Path(out_path)
    # Ensure parent dir exists
    if out_file.parent and not out_file.parent.exists():
        out_file.parent.mkdir(parents=True, exist_ok=True)

    with out_file.open("w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)

    # Clear, friendly multi-line output for demos
    print(f"\033[95mWrote ticket file: {out_file}")
    print("\033[96mTicket Summary:\033[0m")
    print(f"  id:        {data['id']}")
    print(f"  departure: {data['departure-station']}")
    print(f"  arrival:   {data['arrival-station']}")
    print(f"  day:       {data['day-of-travel']}")


if __name__ == "__main__":
    main()
