// PM2.5 levels of the Polish air quality index (GIOS), 1-hour values in µg/m³
export const PM25_LEVELS = [
  { max: 13, label: 'Bardzo dobry', color: '#57b108' },
  { max: 35, label: 'Dobry', color: '#b0dd10' },
  { max: 55, label: 'Umiarkowany', color: '#ffd911' },
  { max: 75, label: 'Dostateczny', color: '#e58100' },
  { max: 110, label: 'Zły', color: '#e50000' },
  { max: Infinity, label: 'Bardzo zły', color: '#990000' },
]

export const NO_DATA_COLOR = '#9e9e9e'
export const ANOMALY_COLOR = '#c62828'

export function levelForPm25(pm25) {
  if (pm25 === null || pm25 === undefined) {
    return null
  }
  return PM25_LEVELS.find((level) => pm25 <= level.max)
}

export function colorForSensor(sensor) {
  if (!sensor.valid) {
    return NO_DATA_COLOR
  }
  const level = levelForPm25(sensor.pm25)
  return level ? level.color : NO_DATA_COLOR
}

export const SOURCE_LABELS = {
  GIOS: 'GIOŚ',
  SENSOR_COMMUNITY: 'Sensor.community',
  AIRLY: 'Airly',
  LOOKO2: 'LookO2',
}

export function formatValue(value) {
  return value === null || value === undefined ? '–' : value.toFixed(1)
}

export function formatTime(isoString) {
  if (!isoString) {
    return 'brak danych'
  }
  return new Date(isoString).toLocaleString('pl-PL', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
