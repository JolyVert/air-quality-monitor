import {
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js'
import { Line } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend)

// measurements rejected by validation are shown as gaps in the line
function MeasurementChart({ measurements }) {
  const labels = measurements.map((m) =>
    new Date(m.measuredAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' }),
  )

  const data = {
    labels,
    datasets: [
      {
        label: 'PM2,5',
        data: measurements.map((m) => (m.valid ? m.pm25 : null)),
        borderColor: '#d32f2f',
        backgroundColor: '#d32f2f',
      },
      {
        label: 'PM10',
        data: measurements.map((m) => (m.valid ? m.pm10 : null)),
        borderColor: '#1976d2',
        backgroundColor: '#1976d2',
      },
    ],
  }

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    elements: { point: { radius: 0 } },
    interaction: { mode: 'index', intersect: false },
    scales: {
      y: { beginAtZero: true, title: { display: true, text: 'µg/m³' } },
      x: { ticks: { maxTicksLimit: 8 } },
    },
  }

  return (
    <div className="chart">
      <Line data={data} options={options} />
    </div>
  )
}

export default MeasurementChart
