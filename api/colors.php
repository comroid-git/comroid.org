<?php
// Check if an image file is uploaded
if(isset($_FILES['image']) && $_FILES['image']['error'] === UPLOAD_ERR_OK) {
    $imagePath = $_FILES['image']['tmp_name'];

    // Create an image resource from the uploaded image
    $image = imagecreatefromstring(file_get_contents($imagePath));

    // Resize the image to a smaller size for faster processing
    $width = imagesx($image);
    $height = imagesy($image);
    $newWidth = 100;
    $newHeight = intval($height * ($newWidth / $width));
    $resizedImage = imagecreatetruecolor($newWidth, $newHeight);
    imagecopyresampled($resizedImage, $image, 0, 0, 0, 0, $newWidth, $newHeight, $width, $height);

    // Analyze the colors in the resized image
    $colors = [];

    for ($x = 0; $x < $newWidth; $x++) {
        for ($y = 0; $y < $newHeight; $y++) {
            $rgb = imagecolorat($resizedImage, $x, $y);
            $color = imagecolorsforindex($resizedImage, $rgb);
            $colorKey = "{$color['red']}-{$color['green']}-{$color['blue']}";

            if (!isset($colors[$colorKey])) {
                $colors[$colorKey] = 1;
            } else {
                $colors[$colorKey]++;
            }
        }
    }

    // Sort the colors by frequency
    arsort($colors);

    // Get the top 5 colors
    $topColors = array_slice($colors, 0, 5, true);

    // Convert color keys back to arrays
    foreach ($topColors as $colorKey => $frequency) {
        [$red, $green, $blue] = explode("-", $colorKey);
        $topColors[$colorKey] = ['red' => $red, 'green' => $green, 'blue' => $blue, 'frequency' => $frequency];
    }

    // Output the top 5 colors as JSON
    header('Content-Type: application/json');
    echo json_encode($topColors, JSON_PRETTY_PRINT);

    // Clean up resources
    imagedestroy($image);
    imagedestroy($resizedImage);
} else {
    echo "Error uploading image.";
}
?>
