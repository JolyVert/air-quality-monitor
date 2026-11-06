import { CircleMarker, MapContainer, TileLayer, Tooltip } from 'react-leaflet'
import { colorForSensor, formatValue, SOURCE_LABELS } from '../airQuality.js'

const WROCLAW_CENTER = [51.11, 17.03]

function SensorMap({ sensors, selectedId, onSelect }) {
  return (
    <MapContainer center={WROCLAW_CENTER} zoom={12} className="map">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {sensors.map((sensor) => (
        <CircleMarker
          key={sensor.id}
          center={[sensor.latitude, sensor.longitude]}
          radius={sensor.id === selectedId ? 11 : 8}
          pathOptions={{
            color: sensor.id === selectedId ? '#000' : '#fff',
            weight: 2,
            fillColor: colorForSensor(sensor),
            fillOpacity: 0.9,
          }}
          eventHandlers={{ click: () => onSelect(sensor.id) }}
        >
          <Tooltip>
            {sensor.name || `Czujnik ${sensor.id}`} ({SOURCE_LABELS[sensor.source]})
            <br />
            PM2,5: {formatValue(sensor.pm25)} µg/m³
            {!sensor.valid && ' (błędny odczyt)'}
          </Tooltip>
        </CircleMarker>
      ))}
    </MapContainer>
  )
}

export default SensorMap
