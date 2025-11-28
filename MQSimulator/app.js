let stompClient = null;
let selectedQueue = null;
const depthChartCtx = document.getElementById('depthChart').getContext('2d');

// Very simple default Chart.js line chart
const chartData = {
  labels: [], // timestamps or indices
  datasets: [{
    label: 'Depth',
    data: [],
    fill: false,
    tension: 0.1
  }]
};

const depthChart = new Chart(depthChartCtx, {
  type: 'line',
  data: chartData,
  options: {
    scales: {
      x: { display: true },
      y: { beginAtZero: true }
    }
  }
});

function connectWebSocket(queueName) {
  if (stompClient) {
    // unsubscribe logic can be added — for simplicity recreate connection
    stompClient.disconnect();
    stompClient = null;
  }

  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // subscribe to depth updates
    stompClient.subscribe('/topic/queue/' + queueName + '/depth', function(message) {
      const ev = JSON.parse(message.body);
      document.getElementById('qDepth').innerText = ev.depth;
      addPointToChart(new Date().toLocaleTimeString(), ev.depth);
    });

    // subscribe to history (initial series)
    stompClient.subscribe('/topic/queue/' + queueName + '/history', function(message) {
      const history = JSON.parse(message.body); // array of integers
      rebuildChart(history);
    });

    // metadata
    stompClient.subscribe('/topic/queue/' + queueName + '/meta', function(message){
      const ev = JSON.parse(message.body);
      document.getElementById('qName').innerText = ev.metadata.name;
      document.getElementById('qType').innerText = ev.metadata.type;
      document.getElementById('qCap').innerText = ev.metadata.maxCapacity;
    });

    // status
    stompClient.subscribe('/topic/queue/' + queueName + '/status', function(message){
      const ev = JSON.parse(message.body);
      document.getElementById('qStatus').innerText = ev.status;
    });

    // message dump
    stompClient.subscribe('/topic/queue/' + queueName + '/dump', function(message){
      const ev = JSON.parse(message.body);
      document.getElementById('dumpArea').innerText = JSON.stringify(ev.messages, null, 2);
    });

    // request server to broadcast initial state (optional)
    // We can't call server via websocket route without additional controller mapping.
    // Instead rely on server broadcasting metadata/history on creation and after operations.
  });
}

function addPointToChart(label, value) {
  chartData.labels.push(label);
  chartData.datasets[0].data.push(value);
  if (chartData.labels.length > 100) {
    chartData.labels.shift();
    chartData.datasets[0].data.shift();
  }
  depthChart.update();
}

function rebuildChart(historyArray) {
  chartData.labels = historyArray.map((_, i) => i.toString());
  chartData.datasets[0].data = historyArray;
  depthChart.update();
}

// queue select handling
function onQueueChange() {
  const sel = document.getElementById('queueSelect');
  const q = sel.value;
  selectedQueue = q;

  // fill hidden inputs with queue
  ['statusQueue','enqueueQueue','dequeueQueue','clearQueue','dumpQueue'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = q;
  });

  // update URL param (nice UX)
  const url = new URL(window.location);
  url.searchParams.set('queue', q);
  window.history.replaceState({}, '', url);

  connectWebSocket(q);
}

// theme toggle
document.addEventListener('DOMContentLoaded', () => {
  // populate queue select with server-side options automatically by Thymeleaf; default pick first
  const sel = document.getElementById('queueSelect');
  if (sel.options.length > 0) {
    const urlParams = new URLSearchParams(window.location.search);
    const pre = urlParams.get('queue');
    if (pre) {
      sel.value = pre;
    }
    onQueueChange();
  }

  const themeCheckbox = document.getElementById('themeCheckbox');
  themeCheckbox.addEventListener('change', (e) => {
    if (e.target.checked) document.documentElement.classList.add('dark');
    else document.documentElement.classList.remove('dark');
  });
});
