if (location.pathname === "/admin") renderPage().then()

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function onError(message) {
  button.ariaBusy = "false"
  infobox.innerHTML = message
  infobox.style.display = "block"
  button.classList.add("contrast")
  await sleep(350)
  button.classList.remove("contrast")
  await sleep (250)
 button.classList.add("contrast")
  await sleep(350)
  button.classList.remove("contrast")
}

const button = document.getElementById("submit")
const infobox = document.getElementById("info")

async function renderPage(token = "") {
  const t = token === undefined || token === "" ?
    document.cookie
    .split("; ")
    .find(row => row.startsWith("token="))
    ?.split("=")[1] : token
  if (t === undefined) return
  const page = await fetch("/admin", {
    headers: {
      "Authorization": "Bearer " + t
    }
  })
  if (page.status !== 200) {
    const error = await page.text()
    return await onError(error === "" ? "An error occurred" : error)
  }
  document.querySelector("html").innerHTML = await page.text()
}

button.onclick = async () =>  {
  const login = document.getElementById("login").value
  const password = document.getElementById("password").value
  if (login === null || login === "" || password === null || password === "") return await onError(button)
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
  const response = await fetch(button.name, body)
  if (response.status !== 200) {
    const error = await response.text()
    return await onError(error === "" ? "An error occurred" : error)
  }
  const json = await response.json()
  if (json["token"] === null || json["url"] === null) {
    return await onError("Error: Token or url of response are null")
  }
  if (json["token"] === "") { // if token is empty, just redirect
    window.open(json["url"], "_self")
  } else {
    if (document.getElementById("remember").checked)
      document.cookie = "token=" + json["token"] + "; Secure;"
    await renderPage(json["token"])
  }
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
    infobox.innerHTML = "Registered successfully!"
    infobox.style.display = "block"
  } else {
    const error = errors[params.get("err")] + params.get("msg")
    onError(error).then()
  }
} else if (params.get("msg") !== null) {
  onError(params.get("msg")).then()
}