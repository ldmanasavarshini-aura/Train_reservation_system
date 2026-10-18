const http = require("http");
const { spawn, spawnSync } = require("child_process");

const port = 8080;

function startServer() {
    const compilation = spawnSync("javac", ["-source", "8", "-target", "8", "TrainReservationSystem.java"], {
        stdio: "inherit"
    });

    if (compilation.error) {
        console.error("Could not run javac. Install a JDK and make sure javac is on PATH.");
        process.exit(1);
    }
    if (compilation.status !== 0) process.exit(compilation.status || 1);

    const server = spawn("java", ["-cp", ".", "TrainReservationSystem"], { stdio: "inherit" });
    server.on("error", error => {
        console.error(`Could not start Java: ${error.message}`);
        process.exitCode = 1;
    });
    server.on("exit", code => {
        process.exitCode = code || 0;
    });
}

const request = http.get(`http://localhost:${port}/`, response => {
    response.resume();
    if (response.statusCode === 200) {
        console.log(`Train Reservation System is already running at http://localhost:${port}/`);
        return;
    }
    startServer();
});

request.setTimeout(1000, () => request.destroy());
request.on("error", startServer);
