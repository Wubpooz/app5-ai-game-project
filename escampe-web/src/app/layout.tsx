import type { Metadata } from "next";
import "../styles/globals.css";

export const metadata: Metadata = {
  title: "Escampe - Modern Abstract Chess Strategy Game",
  description: "Play Escampe, the tactical 6x6 abstract board game. Challenge advanced bots, play local pass-and-play, or analyze your games with a premium chess.com-style review system.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
