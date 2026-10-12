/**
 * Self-check classes, one per workstream: CustomerCheck, MenuCheck, StaffCheck, GameCheck, CliCheck.
 * Each has a public static void main, builds its own objects through the public API, uses assert statements,
 * throws if assertions are disabled, and prints one "OK <name>" line. scripts/replay.sh runs every *Check class with -ea.
 */
package restaurantrush.check;
