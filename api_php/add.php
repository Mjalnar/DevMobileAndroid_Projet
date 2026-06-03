<?php
require_once 'config.php';

$json = file_get_contents('php://input');
$data = json_decode($json, true);

if ($data) {
    $stmt = $pdo->prepare("INSERT INTO entries (title, note, latitude, longitude, address, timestamp) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->execute([
        $data['title'],
        $data['note'],
        $data['latitude'],
        $data['longitude'],
        $data['address'],
        $data['timestamp']
    ]);

    echo json_encode(["remote_id" => $pdo->lastInsertId()]);
}
?>
