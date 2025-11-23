import cv2
import numpy as np
import threading
import sys
import json

# Requires: pip install opencv-python
# Uses your webcam to detect and decode QR codes (not barcodes).
# Press 'q' in the window to quit.
# Type 'r' (then Enter) in the console to reset scanned tickets.
# Type 's' (then Enter) to scan exactly one ticket (then stop scanning, waiting for further inputs).

def normalize_points(points):
    if points is None:
        return []
    pts = np.array(points)
    if pts.ndim == 3:
        return [p.reshape(-1, 2).astype(int) for p in pts]
    if pts.ndim == 2 and pts.shape[0] >= 4:
        return [pts.reshape(-1, 2).astype(int)]
    return []

def get_ticket_id(text):
    try:
        data = json.loads(text)
        if isinstance(data, dict) and 'ticketId' in data:
            return str(data['ticketId'])
    except Exception:
        pass
    return None

def console_monitor(scanned_set, lock, stop_event, scan_event):
    while not stop_event.is_set():
        line = sys.stdin.readline()
        if not line:
            break
        cmd = line.strip().lower()
        if cmd == 'r':
            with lock:
                scanned_set.clear()
            print("Scanned tickets reset")
        elif cmd == 's':
            scan_event.set()
            print("Scan requested. Point a QR code at the camera...")

def main():
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Cannot open camera")
        return

    detector = cv2.QRCodeDetector()
    last_seen = set()         
    lock = threading.Lock()
    stop_event = threading.Event()
    scan_event = threading.Event()  

    t = threading.Thread(target=console_monitor, args=(last_seen, lock, stop_event, scan_event), daemon=True)
    t.start()

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Only attempt decoding when a scan was requested
        decoded_texts = []
        points_list = []

        if scan_event.is_set():
            try:
                res = detector.detectAndDecodeMulti(frame)
                if isinstance(res, tuple) and len(res) == 3:
                    decoded_texts, points, _ = res
                elif isinstance(res, tuple) and len(res) == 4:
                    _, decoded_texts, points, _ = res
                else:
                    decoded_texts = []
                    points = None
                points_list = normalize_points(points)
            except Exception:
                data, points, _ = detector.detectAndDecode(frame)
                if data:
                    decoded_texts = [data]
                    points_list = normalize_points(points)

            if decoded_texts:
                text = decoded_texts[0]
                ticket_id = get_ticket_id(text)

                if ticket_id is not None:
                    with lock:
                        if ticket_id not in last_seen:
                            print("\033[92mTicket valid:", ticket_id, "\033[0m")
                            last_seen.add(ticket_id)
                        else:
                            print("\033[91mTicket already scanned:", ticket_id, "\033[0m")

                pts = points_list[0] if len(points_list) > 0 else None
                label = ticket_id if ticket_id is not None else text
                if pts is not None and len(pts) > 0:
                    cv2.polylines(frame, [pts], isClosed=True, color=(0,255,0), thickness=2)
                    x, y = pts[0]
                    cv2.putText(frame, label, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX,
                                0.6, (0,255,0) if ticket_id is not None else (0,255,255), 2, cv2.LINE_AA)
                else:
                    cv2.putText(frame, label, (10, 30), cv2.FONT_HERSHEY_SIMPLEX,
                                0.6, (0,255,0) if ticket_id is not None else (0,255,255), 2, cv2.LINE_AA)

                if ticket_id is not None:
                    scan_event.clear()
            else:
                cv2.putText(frame, "Scanning for one QR... (press Enter 's' again to re-request)", (10, 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0,255,255), 2, cv2.LINE_AA)

        cv2.imshow("QR Reader (press 'q' to quit)", frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    # Cleanup
    stop_event.set()
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()