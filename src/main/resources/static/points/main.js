function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function onError(button) {
  button.classList.add("contrast")
  await sleep(350)
  button.classList.remove("contrast")
  await sleep (250)
 button.classList.add("contrast")
  await sleep(350)
  button.classList.remove("contrast")
}

const button = document.getElementById("submit")
button.onclick = async () =>  {
  const login = document.getElementById("login").value
  const password = document.getElementById("password").value
  if (login === null || login === "" || password === null || password === "") await onError(button)
  button.ariaBusy = "true"
  const body = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      login: login,
      password: password
    })}
  const response = await fetch("/points/register", body)
  if (response.status !== 200) {
    await onError(button)
  }
  const json = await response.json()
  if (json["token"] === null) await onError(button)
  window.open(json["token"], "_self")
}

const errors = {
  "nostate": "State does not exist",
  "gateway": "Trackmania login sent unexpected response: ",
  "mismatch": "You must provide the same login",
  "badreq": ""
}

const params = new URLSearchParams(document.location.search)
if (params.get("registered") !== null) {
  if (params.get("registered") === "true") {
    document.getElementById("info").innerHTML = "Registered successfully!"
  } else {
    const error = errors[params.get("err")] + params.get("msg")
    document.getElementById("info").innerHTML = "Error: " + error
    onError(button).then()
  }
}