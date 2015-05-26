#!/usr/local/bin/python3

import sys

moves = sys.argv[1]

mask_sum = 0
for move in moves.split(','):
    file = ord(move[0]) - ord('a') + 1
    rank = int(move[1])
    offset = (file - 1) + 8 * (rank - 1)
    mask = 2 ** offset
    mask_sum += mask
mask_sum_hex = "%016x" % mask_sum
print(mask_sum_hex)
