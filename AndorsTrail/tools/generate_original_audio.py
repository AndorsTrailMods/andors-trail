import math
import os
import random
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
    "lead": ((1.0, 0.0, 0.34, 0.0, 0.20, 0.0, 0.14), 0.005, 0.045, 0.0, 0.0),
    "pad": ((1.0, 0.12, 0.035, 0.012), 0.045, 0.12, 0.0, 0.0),
    "bell": ((1.0, 0.0, 0.38, 0.0, 0.17, 0.0, 0.08), 0.002, 0.10, 0.0, 2.8),
    "pluck": ((1.0, 0.42, 0.20, 0.10, 0.05), 0.002, 0.075, 0.0, 3.4),
    "bass": ((1.0, 0.0, 0.10, 0.0, 0.035), 0.004, 0.08, 0.0, 0.4),
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


CHORDS = {
    "Em": (52, 55, 59),
    "C": (48, 52, 55),
    "G": (43, 47, 50),
    "D": (50, 54, 57),
    "Am": (45, 48, 52),
    "Bm": (47, 50, 54),
    "B7": (47, 51, 54),
    "D7": (50, 54, 57),
}

EXPLORATION_SECTIONS = (
    ("G", "D", "Em", "C", "Am", "Em", "C", "D"),
    ("C", "D", "G", "Em", "Am", "Bm", "C", "D"),
    ("G", "D", "Em", "C", "Am", "C", "D", "G"),
)

COMBAT_SECTIONS = (
    ("Em", "Em", "C", "D", "Em", "G", "Am", "B7"),
    ("C", "C", "G", "D", "Am", "Em", "B7", "B7"),
    ("Em", "G", "C", "Am", "Em", "B7", "Em", "Em"),
)

EXPLORATION_MELODIES = (
    ((0, 67, 1), (1, 71, 1), (2, 74, 1.5), (3.5, 79, 2.5),
     (6, 78, 0.7), (6.75, 76, 1.25), (8, 74, 1), (9, 71, 1),
     (10, 69, 1.5), (11.5, 71, 1), (13, 74, 1), (14, 71, 2)),
    ((0, 72, 1), (1, 76, 1), (2, 79, 1.5), (4, 76, 1),
     (5, 74, 1), (6, 71, 1.5), (8, 69, 1), (9, 72, 1),
     (10, 76, 1.5), (12, 74, 1), (13, 71, 1), (14, 69, 2)),
    ((0, 67, 1), (1, 71, 1), (2, 74, 1.5), (3.5, 79, 2.5),
     (6, 78, 0.7), (6.75, 76, 1.25), (8, 74, 1), (9, 71, 1),
     (10, 69, 1), (11, 71, 1), (12, 74, 1), (13, 76, 1),
     (14, 74, 2)),
)

COMBAT_MELODIES = (
    ((0, 64, 0.7), (0.75, 64, 0.7), (1.5, 71, 1), (2.5, 74, 1),
     (3.5, 76, 0.5), (4, 79, 1), (5, 76, 1), (6, 74, 1),
     (7, 71, 1), (8, 67, 0.7), (8.75, 67, 0.7), (9.5, 74, 1),
     (10.5, 76, 1), (11.5, 79, 1), (12.5, 76, 1), (14, 71, 1.5)),
    ((0, 72, 0.75), (1, 76, 0.75), (2, 79, 1), (3, 76, 1),
     (4, 74, 0.75), (5, 71, 0.75), (6, 69, 1), (7, 71, 1),
     (8, 72, 0.75), (9, 76, 0.75), (10, 79, 1), (11, 83, 1),
     (12, 81, 0.75), (13, 79, 0.75), (14, 76, 1.5)),
    ((0, 64, 0.7), (0.75, 64, 0.7), (1.5, 71, 1), (2.5, 74, 1),
     (3.5, 76, 0.5), (4, 79, 1), (5, 76, 1), (6, 74, 1),
     (7, 71, 1), (8, 67, 0.7), (8.75, 67, 0.7), (9.5, 74, 1),
     (10.5, 76, 1), (11.5, 79, 1), (12.5, 76, 1), (14, 71, 1.5)),
)


def add_drum(left, right, start, duration, tone, volume, pan, seed):
    start_frame = max(0, int(start * SAMPLE_RATE))
    frame_count = min(int(duration * SAMPLE_RATE), len(left) - start_frame)
    if frame_count <= 0:
        return
    left_gain = math.sqrt((1.0 - pan) * 0.5)
    right_gain = math.sqrt((1.0 + pan) * 0.5)
    rng = random.Random(seed)
    phase = 0.0
    for index in range(frame_count):
        t = index / SAMPLE_RATE
        falloff = math.exp(-t * (5.0 if tone else 17.0))
        phase += 2.0 * math.pi * (tone * (1.0 - min(0.8, t * 1.8)) if tone else 1.0) / SAMPLE_RATE
        body = math.sin(phase) if tone else rng.uniform(-1.0, 1.0)
        sample = volume * falloff * body
        frame = start_frame + index
        left[frame] += sample * left_gain
        right[frame] += sample * right_gain


def compose_music(filename, tempo, sections, melodies, combat=False):
    beats_per_second = tempo / 60.0
    total_bars = sum(len(section) for section in sections)
    duration = total_bars * 4 / beats_per_second
    frame_count = int(duration * SAMPLE_RATE)
    left = [0.0] * frame_count
    right = [0.0] * frame_count
    absolute_bar = 0
    for section_index, section in enumerate(sections):
        melody = melodies[section_index]
        for bar_index, chord_name in enumerate(section):
            start = absolute_bar * 4 / beats_per_second
            chord = CHORDS[chord_name]
            section_volume = (0.82, 0.90, 0.84)[section_index]
            for voice, note in enumerate(chord):
                render_note(left, right, start, 3.85 / beats_per_second,
                            note + 12, "pad", 0.055 * section_volume,
                            (-0.5, 0.0, 0.5)[voice])
            render_note(left, right, start, 0.82 / beats_per_second,
                        chord[0] - 12, "bass", 0.18 * section_volume, -0.18)
            render_note(left, right, start + 2 / beats_per_second,
                        0.82 / beats_per_second, chord[2] - 12, "bass",
                        0.15 * section_volume, 0.18)

            if combat:
                for beat in (0, 1, 2, 3):
                    note = chord[(beat + bar_index) % 3] + 12
                    render_note(left, right, start + beat / beats_per_second,
                                0.50 / beats_per_second, note, "pluck",
                                0.07 * section_volume, -0.3 if beat % 2 == 0 else 0.3)
                if bar_index % 2 == 0:
                    add_drum(left, right, start, 0.20, 60, 0.10, 0.0,
                             absolute_bar * 8 + 1)
                    add_drum(left, right, start + 2 / beats_per_second,
                             0.12, 0, 0.02, 0.6, absolute_bar * 8 + 2)
            else:
                for beat in (0, 1, 2, 3):
                    note = chord[(beat + bar_index) % 3] + 12
                    render_note(left, right, start + beat / beats_per_second,
                                0.58 / beats_per_second, note, "pluck",
                                0.055 * section_volume, 0.35 if beat % 2 else -0.35)

            for beat, note, note_length in melody:
                if int(beat // 4) == bar_index % 4:
                    phrase_start = beat % 4
                    render_note(left, right, start + phrase_start / beats_per_second,
                                note_length / beats_per_second, note,
                                "lead", 0.14 * section_volume, 0.08)
            if bar_index in (3, 7):
                render_note(left, right, start + 3.1 / beats_per_second,
                            0.75 / beats_per_second, chord[2] + 24, "bell",
                            0.045 * section_volume, -0.2)
            absolute_bar += 1

    write_wave(filename, left, right)


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
    compose_music("music_exploration.wav", 108, EXPLORATION_SECTIONS,
                  EXPLORATION_MELODIES)
    compose_music("music_combat.wav", 136, COMBAT_SECTIONS, COMBAT_MELODIES,
                  combat=True)
    make_effects()


if __name__ == "__main__":
    main()
