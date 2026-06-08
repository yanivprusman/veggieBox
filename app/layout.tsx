import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "veggieBox",
  description: "Delivery route manager for produce-box deliveries (Midreshet Ben-Gurion; multi-business, multi-worker)",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  // NOTE: the web admin deliberately does NOT mount the bind-mounted FeedbackChat
  // widget — it isn't present on the NUC (gitignored bind-mount), which would break
  // the standalone prod build. Issue reporting from the field is the Android app's job.
  return (
    <html lang="he" dir="rtl">
      <body>{children}</body>
    </html>
  );
}
