import { useEffect, useState } from 'react'
import { fetchSensors } from './api.js'
import { SOURCE_LABELS } from './airQuality.js'
import SensorMap from './components/SensorMap.jsx'
import SensorPanel from './components/SensorPanel.jsx'
import Sidebar from './components/Sidebar.jsx'

// the backend fetches Sensor.community every 5 minutes, so there is no point in refreshing more often
const REFRESH_INTERVAL_MS = 5 * 60 * 1000

function App() {
  const [sensors, setSensors] = useState([])
  const [error, setError] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [enabledSources, setEnabledSources] = useState(Object.keys(SOURCE_LABELS))

  useEffect(() => {
    function load() {
      fetchSensors()
        .then((data) => {
          // indoor sensors do not say anything about the air outside
          setSensors(data.filter((sensor) => !sensor.indoor))
          setError(null)
        })
        .catch((e) => setError(e.message))
    }

    load()
    const timer = setInterval(load, REFRESH_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [])

  function toggleSource(source) {
    setEnabledSources((current) =>
      current.includes(source) ? current.filter((s) => s !== source) : [...current, source],
    )
  }

  const visibleSensors = sensors.filter((sensor) => enabledSources.includes(sensor.source))
  const selectedSensor = sensors.find((sensor) => sensor.id === selectedId)

  return (
    <div className="app">
      <header>
        <h1>Jakość powietrza we Wrocławiu</h1>
        {error && <span className="error">Brak połączenia z serwerem: {error}</span>}
      </header>
      <main>
        <Sidebar sensors={sensors} enabledSources={enabledSources} onToggleSource={toggleSource} />
        <SensorMap sensors={visibleSensors} selectedId={selectedId} onSelect={setSelectedId} />
        {selectedSensor && (
          <SensorPanel key={selectedSensor.id} sensor={selectedSensor} onClose={() => setSelectedId(null)} />
        )}
      </main>
    </div>
  )
}

export default App
