function button(index)
{
  fetch('http://localhost:8080/api/button/' + index)
  .then(function(response) {
    return response.json();
  })
  .then(function(myJson) {
    console.log(JSON.stringify(myJson));
  });
}

function init_data(resp, key) {
  var data = {y: [resp[key]],
              mode: 'lines',
              type: 'scatter',
             };
  var div = document.getElementById('plotly_' + i);
  if (div === null)
  {
    div = document.createElement('div');

  }
  div.id = 'plotly_' + i;
  document.body.appendChild(div);
  Plotly.newPlot(div, [data],
                 {'title': key}, {displayModeBar: false});
  return data
}

function report_error(err)
{
  console.log(err);
  document.getElementById('message').innerHTML = err;
}

function update_date()
{
  var d = new Date();
  document.getElementById('message').innerHTML = 'Last updated: ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
}

function start()
{
  var divMessage = document.getElementById('message');
  divMessage.innerHTML = 'started';

  var data = {};
  setInterval(function() {
    fetch('http://localhost:8080/api/measurement')
    .then((resp) => resp.json())
    .then(function(resp) {
      var div = document.getElementById('test');
      update_date();
      for (i in resp)
      {
        if (i === 'timestamp' || i === 'millis')
        {
          continue;
        }
        if (data[i] !== undefined && data[i].y !== undefined)
        {
          data[i].y.push(resp[i]);
          Plotly.extendTraces('plotly_' + i, {y: [[resp[i]]]}, [0]);
        }
        else
        {
          data[i] = init_data(resp, i)
        }
        if (data[i].y.length > 1000)
        {
          data[i] = {}
        }
      }
    })
    .catch(err => {report_error(err)})
  }, 1000);
}
