function switchPanel(panelId, button) {
    document.querySelectorAll('.panel').forEach(panel => panel.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(panelId).classList.add('active');
    button.classList.add('active');
}

// Intercept Ticket Booking
document.getElementById('bookingForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const formData = new URLSearchParams(new FormData(this));
    
    fetch('/book', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(text => handleBackendResponse(text, this))
    .catch(err => alert("❌ Server Connection Error: Ensure backend is running."));
});

// Intercept Admin Train Registration
document.getElementById('adminForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const formData = new URLSearchParams(new FormData(this));
    
    fetch('/update-train', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(text => handleBackendResponse(text, this))
    .catch(err => alert("❌ Server Connection Error: Ensure backend is running."));
});

// Universal Popup Alert Engine
function handleBackendResponse(message, formElement) {
    let cleanMessage = message.replace(/<[^>]*>/g, '').trim();

    if (cleanMessage.startsWith("ERROR:")) {
        alert("❌ Booking/Update Failed!\n\n" + cleanMessage.replace("ERROR:", "").trim());
    } else if (cleanMessage.startsWith("SUCCESS:")) {
        alert("🎉 System Success!\n\n" + cleanMessage.replace("SUCCESS:", "").trim());
        formElement.reset(); 
    } else {
        alert(cleanMessage);
    }
}
