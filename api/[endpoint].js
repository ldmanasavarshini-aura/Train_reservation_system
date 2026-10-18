const trains = new Map([
    ["12345", {
        name: "Express Rail",
        source: "Chennai",
        destination: "Coimbatore",
        totalSeats: 50,
        bookedSeats: 0
    }]
]);

async function readForm(request) {
    if (request.body && typeof request.body === "object") return request.body;
    let body = typeof request.body === "string" ? request.body : "";
    if (!body) {
        const chunks = [];
        for await (const chunk of request) chunks.push(chunk);
        body = Buffer.concat(chunks).toString("utf8");
    }
    return Object.fromEntries(new URLSearchParams(body));
}

function positiveInteger(value) {
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 0;
}

module.exports = async function handler(request, response) {
    response.setHeader("Content-Type", "text/plain; charset=UTF-8");
    if (request.method !== "POST") {
        response.setHeader("Allow", "POST");
        response.status(405).send("Method not allowed");
        return;
    }

    const endpoint = request.url.split("?")[0].split("/").pop();
    const form = await readForm(request);

    if (endpoint === "update-train") {
        const trainNumber = String(form.trainNumber || "").trim();
        const trainName = String(form.trainName || "").trim();
        const source = String(form.source || "").trim();
        const destination = String(form.destination || "").trim();
        const totalSeats = positiveInteger(form.totalSeats);
        if (!trainNumber || !trainName || !source || !destination || !totalSeats) {
            response.status(400).send("ERROR: Enter valid train details and a positive seat capacity.");
            return;
        }
        trains.set(trainNumber, { name: trainName, source, destination, totalSeats, bookedSeats: 0 });
        response.status(200).send(`SUCCESS: Train ${trainNumber} setup complete. Total capacity allocated: ${totalSeats} seats.`);
        return;
    }

    if (endpoint === "book") {
        const passenger = String(form.passengerName || "").trim();
        const trainNumber = String(form.trainNumber || "").trim();
        const seatCount = positiveInteger(form.seatCount);
        if (!passenger || !trainNumber || !seatCount) {
            response.status(400).send("ERROR: Enter a passenger name, train number, and positive seat count.");
            return;
        }
        const train = trains.get(trainNumber);
        if (!train) {
            response.status(404).send(`ERROR: The entered Train Number (${trainNumber}) cannot be found in our schedules.`);
            return;
        }
        const availableSeats = train.totalSeats - train.bookedSeats;
        if (availableSeats < seatCount) {
            response.status(409).send(`ERROR: Insufficient seat availability. You requested ${seatCount} seats, but only ${availableSeats} are left.`);
            return;
        }
        train.bookedSeats += seatCount;
        response.status(200).send(`SUCCESS: Successfully booked ${seatCount} ticket(s) for ${passenger} on Train ${trainNumber} (${train.name}). Remaining open seats left: ${train.totalSeats - train.bookedSeats}`);
        return;
    }

    response.status(404).send("Not found");
};
