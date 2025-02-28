var socket = new SockJS("/ws");
var stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log("✅ Kết nối WebSocket thành công:", frame);

    // Lắng nghe tin nhắn gửi đến
    stompClient.subscribe("/user/queue/messages", function (message) {
        var receivedMessage = JSON.parse(message.body);
        console.log("📩 Nhận tin nhắn:", receivedMessage);

        appendMessage(receivedMessage.senderId, receivedMessage.content, receivedMessage.timestamp);
    });
});

$("#messageForm").on("submit", function (event) {
    event.preventDefault(); // Ngăn chặn reload trang

    var senderId = $("#currentUserID").val();
    var recipientId = $("#recipientID").val();
    var messageContent = $("#messageText").val();

    var message = {
        senderId: senderId,
        recipientId: recipientId,
        content: messageContent,
        timestamp: new Date().toISOString() // Gửi timestamp chính xác
    };

    stompClient.send("/app/chat", {}, JSON.stringify(message)); // Gửi tin nhắn lên server

    // 🚀 Hiển thị ngay tin nhắn trên giao diện
    appendMessage(senderId, messageContent, message.timestamp);

    $("#messageText").val(""); // Xóa nội dung input
});

// ✅ Hàm hiển thị tin nhắn ngay lập tức
function appendMessage(senderId, content, timestamp) {
    var sender = senderId === $("#currentUserID").val() ? "Bạn" : "Người kia";

    var messageHTML = `
        <div class="message-box ${sender === 'Bạn' ? 'right' : 'left'}">
            <div class="message-header">${sender}</div>
            <div class="message-time">${new Date(timestamp).toLocaleString()}</div>
            <div class="message-text">${content}</div>
        </div>
    `;

    $("#messagesContainer").append(messageHTML);
    scrollToBottom();
}

// ✅ Cuộn xuống tin nhắn mới nhất
function scrollToBottom() {
    $("#messagesContainer").scrollTop($("#messagesContainer")[0].scrollHeight);
}
