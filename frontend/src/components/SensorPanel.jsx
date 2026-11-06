import { useEffect, useState } from 'react'
import { fetchMeasurements } from '../api.js'
import { formatTime, formatValue, levelForPm25, SOURCE_LABELS } from '../airQuality.js'
import MeasurementChart from './MeasurementChart.jsx'

// App renders it with key={sensor.id}, so the state starts empty for every sensor
function SensorPanel({ sensor, onClose }) {
  const [measurements, setMeasurements] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchMeasurements(sensor.id)
      .then(setMeasurements)
      .catch((e) => setError(e.message))
  }, [sensor.id])

  const level = levelForPm25(sensor.pm25)

  return (
    <div className="panel">
      <button className="close" onClick={onClose} title="Zamknij">×</button>
      <h2>{sensor.name || `Czujnik ${sensor.id}`}</h2>
      <p className="muted">
        {SOURCE_LABELS[sensor.source]}
        {sensor.sensorType && `, ${sensor.sensorType}`}
      </p>

      <div className="values">
        <div>
          <span className="value">{formatValue(sensor.pm25)}</span>
          <span className="muted"> PM2,5 µg/m³</span>
        </div>
        <div>
          <span className="value">{formatValue(sensor.pm10)}</span>
          <span className="muted"> PM10 µg/m³</span>
        </div>
      </div>
      <p>
        {level && <span className="badge" style={{ background: level.color }}>{level.label}</span>}
        <span className="muted"> pomiar: {formatTime(sensor.measuredAt)}</span>
      </p>

      <h3>Ostatnie 24 godziny</h3>
      {error && <p className="error">Nie udało się pobrać danych: {error}</p>}
      {!error && measurements === null && <p className="muted">Ładowanie...</p>}
      {measurements && measurements.length === 0 && <p className="muted">Brak pomiarów.</p>}
      {measurements && measurements.length > 0 && <MeasurementChart measurements={measurements} />}
    </div>
  )
}

export default SensorPanel
