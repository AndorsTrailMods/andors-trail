import math
import os
import struct
import wave


SAMPLE_RATE = 22050
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "res", "raw")


def midi_frequency(note):
    return 440.0 * (2.0 ** ((note - 69) / 12.0))


def write_wave(filename, samples):
    peak = max(max(abs(sample) for sample in samples), 1e-9)
    scale = min(0.92 / peak, 1.0)
    path = os.path.join(OUTPUT_DIR, filename)
    with wave.open(path, "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, sample * scale)) * 32767))
            for sample in samples
        ))


def envelope(position, duration, attack=0.012, release=0.045):
    return min(1.0, position / attack, (duration - position) / release)


def synth_note(frequency, duration, waveform="square", volume=0.2):
    count = int(duration * SAMPLE_RATE)
    samples = []
    for index in range(count):
        position = index / SAMPLE_RATE
        phase = 2.0 * math.pi * frequency * position
        if waveform == "square":
            value = 1.0 if math.sin(phase) >= 0 else -1.0
            value = value * 0.72 + math.sin(phase) * 0.28
        elif waveform == "triangle":
            value = 2.0 / math.pi * math.asin(math.sin(phase))
        else:
            value = math.sin(phase)
        samples.append(value * volume * max(0.0, envelope(position, duration)))
    return samples


def add_note(destination, note, start, duration, waveform, volume):
    if note is None:
        return
    start_index = int(start * SAMPLE_RATE)
    note_samples = synth_note(midi_frequency(note), duration, waveform, volume)
    for index, sample in enumerate(note_samples):
        target = start_index + index
        if target < len(destination):
            destination[target] += sample


def make_music(filename, tempo, melody, bass, chords):
    beat = 60.0 / tempo
    step = beat / 2.0
    duration = 8 * 4 * beat
    samples = [0.0] * int(duration * SAMPLE_RATE)
    for index, note in enumerate(melody):
        add_note(samples, note, index * step, step * 0.88, "square", 0.105)
    for index, note in enumerate(bass):
        add_note(samples, note, index * beat, beat * 0.9, "triangle", 0.16)
    for bar in range(8):
        chord = chords[bar % len(chords)]
        for beat_index, note in enumerate(chord):
            add_note(samples, note, bar * beat * 4 + beat_index * beat,
                     beat * 0.72, "triangle", 0.045)
    write_wave(filename, samples)


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
            value = (1.0 if math.sin(phase) >= 0 else -1.0) * 0.7 + math.sin(phase) * 0.3
        else:
            value = math.sin(phase)
        samples.append(value * volume * max(0.0, envelope(position, duration, 0.004, 0.06)))
    return samples


def make_effects():
    hit_samples = make_sweep(155, 62, 0.19, "square", 0.65)
    hit_ring = make_sweep(820, 310, 0.15, "sine", 0.28)
    hit_samples.extend([0.0] * max(0, len(hit_ring) - len(hit_samples)))
    for index, sample in enumerate(hit_ring):
        hit_samples[index] += sample
    write_wave("sfx_hit.wav", hit_samples)

    write_wave("sfx_miss.wav", make_sweep(680, 245, 0.16, "square", 0.32))

    monster_hit = make_sweep(105, 48, 0.23, "square", 0.55)
    monster_ring = make_sweep(390, 130, 0.2, "sine", 0.18)
    monster_hit.extend([0.0] * max(0, len(monster_ring) - len(monster_hit)))
    for index, sample in enumerate(monster_ring):
        monster_hit[index] += sample
    write_wave("sfx_monster_hit.wav", monster_hit)

    write_wave("sfx_monster_miss.wav", make_sweep(460, 150, 0.22, "sine", 0.38))

    magic = [0.0] * int(0.42 * SAMPLE_RATE)
    for start, note in ((0.0, 67), (0.1, 74), (0.2, 79), (0.3, 86)):
        add_note(magic, note, start, 0.13, "square", 0.34)
    write_wave("sfx_magic.wav", magic)

    mark = make_sweep(360, 980, 0.24, "sine", 0.35)
    mark_ring = make_sweep(1180, 760, 0.2, "triangle", 0.16)
    mark.extend([0.0] * max(0, len(mark_ring) - len(mark)))
    for index, sample in enumerate(mark_ring):
        target = int(0.08 * SAMPLE_RATE) + index
        if target < len(mark):
            mark[target] += sample
    write_wave("sfx_ravenmark.wav", mark)

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    make_music(
        "music_exploration.wav", 104,
        [74, None, 77, 81, None, 79, 77, None, 72, None, 74, 77, 81, None, 84, None,
         81, None, 79, 77, None, 74, 72, None, 69, None, 72, 74, 77, None, 74, None,
         74, None, 77, 81, None, 84, 81, None, 79, None, 77, 74, 72, None, 74, None,
         77, None, 81, 84, None, 81, 79, None, 77, None, 74, 72, 69, None, 72, None],
        [50, 57, 53, 60, 48, 55, 52, 59, 45, 52, 48, 55, 47, 54, 50, 57,
         50, 57, 53, 60, 48, 55, 52, 59, 45, 52, 48, 55, 43, 50, 47, 54],
        [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]],
    )
    make_music(
        "music_combat.wav", 132,
        [74, 74, None, 77, 81, None, 77, 74, 72, 72, None, 76, 79, None, 76, 72,
         69, 69, None, 72, 77, None, 72, 69, 67, 67, None, 71, 74, None, 71, 67,
         74, 74, None, 77, 81, None, 84, 81, 79, 79, None, 77, 74, None, 77, 79,
         81, 81, None, 79, 77, None, 74, 72, 71, 71, None, 74, 77, None, 74, 71],
        [38, 38, 38, 38, 41, 41, 41, 41, 43, 43, 43, 43, 36, 36, 36, 36,
         38, 38, 38, 38, 41, 41, 41, 41, 43, 43, 43, 43, 45, 45, 45, 45],
        [[50, 53, 57], [53, 57, 60], [55, 59, 62], [48, 52, 55]],
    )
    make_effects()


if __name__ == "__main__":
    main()
