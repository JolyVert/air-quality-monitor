import { ANOMALY_COLOR, NO_DATA_COLOR, PM25_LEVELS, SOURCE_LABELS } from '../airQuality.js'

function Sidebar({ sensors, enabledSources, onToggleSource }) {
  const countBySource = {}
  for (const sensor of sensors) {
    countBySource[sensor.source] = (countBySource[sensor.source] || 0) + 1
  }

  return (
    <div className="sidebar">
      <h3>Źródła danych</h3>
      {Object.entries(SOURCE_LABELS).map(([source, label]) => (
        <label key={source} className="source">
          <input
            type="checkbox"
            checked={enabledSources.includes(source)}
            onChange={() => onToggleSource(source)}
          />
          {label} <span className="muted">({countBySource[source] || 0})</span>
        </label>
      ))}

      <h3>PM2,5 [µg/m³]</h3>
      {PM25_LEVELS.map((level, i) => (
        <div key={level.label} className="legend-row">
          <span className="dot" style={{ background: level.color }} />
          {level.label}
          <span className="muted">
            {' '}
            {i === 0 ? 0 : PM25_LEVELS[i - 1].max}
            {level.max === Infinity ? '+' : `–${level.max}`}
          </span>
        </div>
      ))}
      <div className="legend-row">
        <span className="dot" style={{ background: NO_DATA_COLOR }} />
        Brak danych lub błędny odczyt
      </div>
      <div className="legend-row">
        <span className="dot anomaly" style={{ borderColor: ANOMALY_COLOR }} />
        Anomalia lokalna
        <span className="muted"> ({sensors.filter((sensor) => sensor.anomaly).length})</span>
      </div>
    </div>
  )
}

export default Sidebar
