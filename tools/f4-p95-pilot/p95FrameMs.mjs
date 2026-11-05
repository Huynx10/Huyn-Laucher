export function p95FrameMs(samples) {
  if (
    !Array.isArray(samples) ||
    samples.length === 0 ||
    !Array.from(samples).every(
      (value) => typeof value === "number" && Number.isFinite(value) && value >= 0
    )
  ) {
    return null;
  }

  const sorted = [...samples].sort((a, b) => a - b);
  return sorted[Math.ceil(0.95 * sorted.length) - 1];
}
