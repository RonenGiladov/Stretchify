const demoBuildId = window.stretchifyDemoBuildId;
const demoDialog = document.querySelector("#demo-dialog");
const emulatorContainer = document.querySelector("#emulator-container");
const demoStatus = document.querySelector("#demo-status");
let demoTimeout;

if (typeof demoBuildId === "string" && /^[a-zA-Z0-9_-]+$/.test(demoBuildId)) {
  const demoUrl = `https://appetize.io/app/${encodeURIComponent(demoBuildId)}`;
  document.querySelector("#external-demo").href = demoUrl;
  document.querySelector(".explore-link").hidden = true;

  document.querySelectorAll(".try-demo").forEach((button) => {
    button.hidden = false;
    button.addEventListener("click", () => {
      demoDialog.showModal();
      demoStatus.textContent = "Starting the browser demo…";
      const emulator = document.createElement("iframe");
      emulator.title = "Interactive Stretchify Android demo";
      emulator.src = `https://appetize.io/embed/${encodeURIComponent(demoBuildId)}` +
        "?device=pixel7&osVersion=13.0&scale=auto";
      emulator.allow = "fullscreen";
      emulator.addEventListener("load", () => {
        clearTimeout(demoTimeout);
        demoStatus.textContent = "Use the emulator controls to start. You can also open the demo separately.";
      });
      emulatorContainer.replaceChildren(emulator);
      demoTimeout = setTimeout(() => {
        demoStatus.textContent = "Taking a while? Open the demo separately or watch the walkthrough below.";
      }, 15000);
    });
  });
}

document.querySelector("#close-demo").addEventListener("click", () => demoDialog.close());
document.querySelector("#watch-instead").addEventListener("click", () => demoDialog.close());
demoDialog.addEventListener("close", () => {
  clearTimeout(demoTimeout);
  emulatorContainer.replaceChildren();
});
