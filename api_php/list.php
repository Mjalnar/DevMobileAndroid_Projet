<?php
require_once 'config.php';

$stmt = $pdo->query("SELECT * FROM entries ORDER BY timestamp DESC");
$entries = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo json_encode($entries);
?>
