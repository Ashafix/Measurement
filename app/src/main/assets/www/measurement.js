function button(index) {
  fetch('http://localhost:8080/api/button/' + index)
  .then(function(response) {
    return response.json();
  })
}

function init_data(resp, key) {
  var data = {y: [resp[key]],
              mode: 'lines',
              type: 'scatter',
             };
  if (resp['timestamp'] !== undefined) {
    data.x = [resp['timestamp']]
  } else if (resp['millis'] !== undefined) {
    data.x = [resp['millis']]
  }
  var div = document.getElementById('plotly_' + i);
  if (div === null) {
    div = document.createElement('div');
  }
  div.id = 'plotly_' + i;
  document.body.appendChild(div);
  Plotly.newPlot(div, [data],
                 {'title': key}, {displayModeBar: false});
  return data
}

function report_error(err) {
  console.error(err);
  document.getElementById('message').innerHTML = err;
}

function update_date(toBeUpdated) {
  if (!toBeUpdated)
  {
    return;
  }
  var d = new Date();
  document.getElementById('message').innerHTML = 'Last updated: ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
}

function start() {
  var divMessage = document.getElementById('message');
  divMessage.innerHTML = 'started';

  var data = {};
  var last_data = {};
  setInterval(function() {
    fetch('http://localhost:8080/api/measurement')
    .then((resp) => resp.json())
    .then(function(resp) {
      var updated = false;
      var date_found = false;
      for (i in resp) {
        if (i === 'timestamp' || i === 'millis') {
          updated = (resp[i] != last_data[i]);
          date_found = true;
          if (updated) {
            last_data[i] = resp[i];
            break;
          }
        }
      }

      update_date(updated || !date_found);

      for (i in resp) {
        if (!updated || i === 'timestamp' || i === 'millis') {
          continue;
        }
        if (data[i] !== undefined && data[i].y !== undefined) {
          data[i].y.push(resp[i]);

          if (data[i].x !== undefined) {
            if (resp['timestamp'] !== undefined) {
              data[i].x.push(resp['timestamp'])
            } else if (resp['millis'] !== undefined) {
              data[i].x.push(resp['millis'])
            }
          }
          if (date_found) {
            Plotly.extendTraces('plotly_' + i, {y: [[resp[i]]], x: [[data[i].x[data[i].x.length - 1]]]}, [0]);
          } else {
            Plotly.extendTraces('plotly_' + i, {y: [[resp[i]]]}, [0]);
          }

        }
        else {
          data[i] = init_data(resp, i)
        }
        if (data[i].y.length > 1000) {
          data[i] = {}
        }
      }
    })
    .catch(err => {report_error(err)})
  }, 1000);
}
