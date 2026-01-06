<?php
/**
 * Audio Component
 * Helper for playing audio instructions on landing pages
 */
?>
<script>
let isAudioPlaying = false;

function playLanderAudio(file) {
    if (isAudioPlaying) return;
    
    // Stop all other media
    document.querySelectorAll('audio, video').forEach(media => {
        media.pause();
        media.currentTime = 0;
    });

    const audio = new Audio('/assets/' + file + '.mp3');
    isAudioPlaying = true;
    
    audio.addEventListener('ended', () => {
        isAudioPlaying = false;
    });

    audio.play().catch(e => console.log("Audio play blocked by browser."));
}
</script>
