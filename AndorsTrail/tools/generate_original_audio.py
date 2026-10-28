import math
import os
import struct
import wave


SAMPLE_RATE = 22050
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "res", "raw")
SILENCE_DB = 1e-9


def midi_frequency(note):
    return 440.0 * (2.0 ** ((note - 69) / 12.0))


def write_wave(filename, left, right=None):
    channels = [left] if right is None else [left, right]
    peak = max((max(abs(sample) for sample in channel) for channel in channels), default=0.0)
    scale = min(0.88 / max(peak, SILENCE_DB), 1.0)
    path = os.path.join(OUTPUT_DIR, filename)
    with wave.open(path, "wb") as output:
        output.setnchannels(len(channels))
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        if right is None:
            output.writeframes(b"".join(
                struct.pack("<h", int(max(-1.0, min(1.0, sample * scale)) * 32767))
                for sample in left
            ))
        else:
            output.writeframes(b"".join(
                struct.pack("<hh",
                            int(max(-1.0, min(1.0, l * scale)) * 32767),
                            int(max(-1.0, min(1.0, r * scale)) * 32767))
                for l, r in zip(left, right)
            ))


INSTRUMENTS = {
    "flute": ((1.0, 0.14, 0.035, 0.012), 0.055, 0.16, 0.13, 0.0),
}


def render_note(left, right, start, duration, note, instrument, volume, pan):
    harmonics, attack, release, vibrato, decay = INSTRUMENTS[instrument]
    start_frame = max(0, int(start * SAMPLE_RATE))
    frame_count = int(duration * SAMPLE_RATE)
    if start_frame >= len(left):
        return
    frame_count = min(frame_count, len(left) - start_frame)
    frequency = midi_frequency(note)
    left_gain = math.sqrt((1.0 - pan) * 0.5)
    right_gain = math.sqrt((1.0 + pan) * 0.5)
    phases = [0.0] * len(harmonics)
    for index in range(frame_count):
        t = index / SAMPLE_RATE
        if t < attack:
            envelope = t / attack
        elif t > duration - release:
            envelope = max(0.0, (duration - t) / release)
        else:
            envelope = 1.0
        if decay:
            envelope *= math.exp(-decay * t)
        vibrato_scale = 1.0
        if vibrato and t > 0.18:
            vibrato_scale += vibrato * math.sin(2.0 * math.pi * 5.2 * (t - 0.18))
        sample = 0.0
        for harmonic_index, amplitude in enumerate(harmonics):
            phases[harmonic_index] += (
                2.0 * math.pi * frequency * (harmonic_index + 1) * vibrato_scale / SAMPLE_RATE
            )
            sample += amplitude * math.sin(phases[harmonic_index])
        sample *= volume * envelope
        frame = start_frame + index
        left[frame] += sample * left_gain
        right[frame] += sample * right_gain


def make_sweep(start_frequency, end_frequency, duration, waveform, volume):
    count = int(duration * SAMPLE_RATE)
    samples = []
    phase = 0.0
    for index in range(count):
        position = index / SAMPLE_RATE
        progress = index / max(1, count - 1)
        frequency = start_frequency + (end_frequency - start_frequency) * progress
        phase += 2.0 * math.pi * frequency / SAMPLE_RATE
        if waveform == "square":
            value = (1.0 if math.sin(phase) >= 0 else -1.0) * 0.55 + math.sin(phase) * 0.45
        else:
            value = math.sin(phase)
        attack = min(1.0, position / 0.008)
        release = min(1.0, (duration - position) / 0.075)
        samples.append(value * volume * max(0.0, attack) * max(0.0, release))
    return samples


def make_effects():
    hit = make_sweep(125, 52, 0.24, "square", 0.46)
    ring = make_sweep(780, 280, 0.38, "sine", 0.18)
    hit.extend([0.0] * max(0, len(ring) - len(hit)))
    for index, sample in enumerate(ring):
        hit[index] += sample
    write_wave("sfx_hit.wav", hit)

    miss = make_sweep(720, 150, 0.32, "sine", 0.28)
    write_wave("sfx_miss.wav", miss)

    monster_hit = make_sweep(92, 38, 0.34, "square", 0.44)
    impact_ring = make_sweep(330, 112, 0.38, "sine", 0.13)
    monster_hit.extend([0.0] * max(0, len(impact_ring) - len(monster_hit)))
    for index, sample in enumerate(impact_ring):
        monster_hit[index] += sample
    write_wave("sfx_monster_hit.wav", monster_hit)

    write_wave("sfx_monster_miss.wav", make_sweep(480, 110, 0.36, "sine", 0.30))

    magic_duration = 0.78
    magic = [0.0] * int(magic_duration * SAMPLE_RATE)
    for start, note in ((0.0, 62), (0.10, 69), (0.20, 74), (0.34, 81), (0.48, 86)):
        render_note(magic, magic, start, 0.24, note, "flute", 0.16, 0.0)
    write_wave("sfx_magic.wav", magic)

    mark_duration = 0.72
    mark = [0.0] * int(mark_duration * SAMPLE_RATE)
    for start, note in ((0.0, 57), (0.10, 64), (0.22, 69), (0.36, 76)):
        render_note(mark, mark, start, 0.30, note, "flute", 0.15, 0.0)
    write_wave("sfx_ravenmark.wav", mark)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    make_effects()


if __name__ == "__main__":
    main()
