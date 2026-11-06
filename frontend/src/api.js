async function getJson(url) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`${url}: HTTP ${response.status}`)
  }
  return response.json()
}

export function fetchSensors() {
  return getJson('/api/sensors')
}

export function fetchMeasurements(sensorId, hours = 24) {
  return getJson(`/api/sensors/${sensorId}/measurements?hours=${hours}`)
}
