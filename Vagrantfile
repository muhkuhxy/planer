# -*- mode: ruby -*-
# vi: set ft=ruby :

# For a complete reference, please see the online documentation at
# https://docs.vagrantup.com.
Vagrant.configure("2") do |config|

   config.vm.define "planer-dev" do |dev|
      dev.vm.provider "docker" do |d|
         d.create_args = ["-dit"]
         d.name = "planer-dev"
         d.build_dir = "."
         d.ports = ["9000:9000"]
         d.link("planer-db:planer-db")
      end
   end

   config.vm.define "planer-db" do |db|
      db.vm.provider "docker" do |d|
         d.name = "planer-db"
         d.image = "postgres"
         d.env = {
            "POSTGRES_USER" => "tloeffen",
            "POSTGRES_PASSWORD" => "gheeim",
            "POSTGRES_DB" => "planer"
         }
         d.ports = ["5432:5432"]
      end
   end
end
