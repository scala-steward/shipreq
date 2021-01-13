module Diff
  Line = Struct.new(:number, :text)

  def self.lines(document)
    document = document.lines if document.is_a?(String)
    document.map.with_index { |text, i| Line.new(i + 1, text) }
  end
end

Slice = Struct.new(:a_low, :a_high, :b_low, :b_high) do
  def a_range
    a_low ... a_high
  end

  def b_range
    b_low ... b_high
  end

  def not_empty?
    a_low < a_high and b_low < b_high
  end
end

Match = Struct.new(:a_line, :b_line) do
  attr_accessor :prev, :next
end

class Patience

  def self.diff(a, b, fallback)
    slice = Slice.new(0, a.size, 0, b.size)
    new(fallback, a, b).diff(slice)
  end

  def initialize(fallback, a, b)
    @fallback = fallback
    @a, @b    = a, b

    @a.each { |x| puts x }

    @indent = ""
  end

  def unique_matching_lines(slice)
    counts = Hash.new { |h, text| h[text] = [0, 0, nil, nil] }

    slice.a_range.each do |n|
      text = @a[n].text
      counts[text][0]  += 1
      counts[text][2] ||= n
    end

    slice.b_range.each do |n|
      text = @b[n].text
      counts[text][1]  += 1
      counts[text][3] ||= n
    end

    counts.select! { |text, count| count.take(2) == [1, 1] }

    counts.map do |text, (n, m, a_line, b_line)|
      Match.new(a_line, b_line)
    end
  end

  def patience_sort(matches)
    stacks = []

    matches.each do |match|
      i = binary_search(stacks, match)
      match.prev = stacks[i] if i >= 0
      stacks[i + 1] = match
    end

    match = stacks.last
    return nil unless match

    while match.prev
      match.prev.next = match
      match = match.prev
    end

    match
  end

  def binary_search(stacks, match)
    low, high = -1, stacks.size

    while low + 1 < high
      mid = (low + high) / 2
      if stacks[mid].b_line < match.b_line
        low = mid
      else
        high = mid
      end
    end

    low
  end

  def diff(slice)
    match = patience_sort(unique_matching_lines(slice))
    puts(@indent + "> diff: " + slice.to_s + " --> " + match.to_s)

    return fallback_diff(slice) unless match

    lines = []
    a_line, b_line = slice.a_low, slice.b_low

    loop do
      puts(@indent + "a/b = " + a_line.to_s + "/" + b_line.to_s)
      a_next, b_next = match ?
                       [match.a_line, match.b_line] :
                       [slice.a_high, slice.b_high]

     subslice   = Slice.new(a_line, a_next, b_line, b_next)
     head, tail = [], []

     match_head(subslice) { |edit| head = head + [edit] }
     match_tail(subslice) { |edit| tail = [edit] + tail }

     @indent = @indent + "  "
     diff(subslice)
     puts(@indent + "<")
     @indent = @indent.slice(2,999999)
     #  lines += head + diff(subslice) + tail
     puts(@indent + "return") unless match
     return lines unless match

      # lines << Diff::Edit.new(:eql, @a[match.a_line], @b[match.b_line])

      a_line, b_line = match.a_line + 1, match.b_line + 1
      match = match.next
      puts(@indent + "m = " + match.to_s) unless match
    end
  end

  def match_head(slice)
    while slice.not_empty? and @a[slice.a_low].text == @b[slice.b_low].text
      yield Diff::Edit.new(:eql, @a[slice.a_low], @b[slice.b_low])
      slice.a_low += 1
      slice.b_low += 1
    end
  end

  def match_tail(slice)
    while slice.not_empty? and @a[slice.a_high - 1].text == @b[slice.b_high - 1].text
      slice.a_high -= 1
      slice.b_high -= 1
      yield Diff::Edit.new(:eql, @a[slice.a_high], @b[slice.b_high])
    end
  end

  def fallback_diff(slice)
    # @fallback.diff(@a[slice.a_range], @b[slice.b_range])
    puts(@indent + "fallback: " + slice.to_s)
  end
end

puts("Starting...")

# a = Diff.lines("CBBC\nADBABCCB\nAAB\nDCCD\nADA\n\nCA\nADBBBBA\nCCBCDAC\nABCAABDC\nC")
# b = Diff.lines("DCCD\nB\nAD\nCBAB\nC\nC\nCCDCAC\nADCBC\nBDD\nDCA\nDAD\nAABCDDD")

a = [
  Line.new(i + 1, text)
]
c = Patience.diff(a, b, nil)
puts("Done.")