<?php
require_once 'config.php';

if (isset($_GET['id'])) {
    $stmt = $pdo->prepare("DELETE FROM entries WHERE id = ?");
    $stmt->execute([$_GET['id']]);
    echo json_encode(["success" => true]);
}
?>
