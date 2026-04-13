const express = require('express');
const client = require('prom-client');

const app = express();
const port = 3000;

// Create a Registry
const register = new client.Registry();

// Default metrics (CPU, memory, etc.)
client.collectDefaultMetrics({ register });

// Custom metric
const httpRequestCounter = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
});

register.registerMetric(httpRequestCounter);

// Routes
app.get('/', (req, res) => {
  httpRequestCounter.inc();
  res.send('🚀 DevOpsssss Demooooo App Running!');
});

app.get('/health', (req, res) => {
  res.status(200).send('OK');
});

// Prometheus metrics endpoint
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

app.listen(port, '0.0.0.0', () => {
  console.log(`App running on http://0.0.0.0:${port}`);
});

// changes for test