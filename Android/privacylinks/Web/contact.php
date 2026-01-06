<?php
$messageSent = false;
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // In a real scenario, you would send an email here using mail() or PHPMailer
    // For now, we will simulate a successful send.
    $messageSent = true;
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contact Us - Private Files Access & Share</title>
    <link rel="stylesheet" href="style.css">
    <style>
        .form-group {
            margin-bottom: 1.5rem;
            text-align: left;
        }
        label {
            display: block;
            margin-bottom: 0.5rem;
            color: var(--text-color);
        }
        input, textarea {
            width: 100%;
            padding: 0.8rem;
            border-radius: 8px;
            border: 1px solid #333;
            background-color: #2C2C2C;
            color: #fff;
            box-sizing: border-box;
            font-family: inherit;
        }
        input:focus, textarea:focus {
            outline: none;
            border-color: var(--primary-color);
        }
        .success-message {
            background-color: rgba(3, 218, 198, 0.1); /* Teal with opacity */
            border: 1px solid var(--secondary-color);
            color: var(--secondary-color);
            padding: 1rem;
            border-radius: 8px;
            margin-bottom: 2rem;
            text-align: center;
        }
    </style>
</head>
<body>
    <header>
        <h1>Private Files Access & Share</h1>
        <nav>
            <a href="index.html">Home</a>
            <a href="privacy-policy.html">Privacy</a>
            <a href="terms-of-usage.html">Terms</a>
            <a href="delete-account.html">Data</a>
            <a href="contact.php">Contact</a>
        </nav>
    </header>

    <main>
        <section class="card">
            <h2>Contact Us</h2>
            
            <?php if ($messageSent): ?>
                <div class="success-message">
                    Thank you for contacting us! We have received your message and will get back to you shortly.
                </div>
            <?php else: ?>
                <p style="margin-bottom: 2rem;">Have questions or feedback? Fill out the form below or email us directly at <a href="mailto:support@pokipro.com" style="color: var(--primary-color);">support@pokipro.com</a>.</p>

                <form method="POST" action="contact.php">
                    <div class="form-group">
                        <label for="name">Name</label>
                        <input type="text" id="name" name="name" required placeholder="Your Name">
                    </div>
                    
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" required placeholder="your.email@example.com">
                    </div>
                    
                    <div class="form-group">
                        <label for="subject">Subject</label>
                        <input type="text" id="subject" name="subject" required placeholder="How can we help?">
                    </div>
                    
                    <div class="form-group">
                        <label for="message">Message</label>
                        <textarea id="message" name="message" rows="5" required placeholder="Describe your issue or feedback..."></textarea>
                    </div>

                    <div style="text-align: center;">
                        <button type="submit" class="btn">Send Message</button>
                    </div>
                </form>
            <?php endif; ?>
        </section>
    </main>

    <footer>
        <p>&copy; 2026 Private Files Access & Share. All rights reserved.</p>
    </footer>
</body>
</html>
