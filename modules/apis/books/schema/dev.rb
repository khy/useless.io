#!/usr/bin/env ruby
# == Wrapper script to update a local postgrseql database
#
# == Usage
#  ./dev.rb
#

Dir.chdir(File.dirname($0)) {
  command = "sem-apply --url postgresql://postgres@localhost/useless_books_dev"
  puts command
  exec(command)
}
