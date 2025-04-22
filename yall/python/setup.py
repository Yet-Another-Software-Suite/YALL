from setuptools import find_packages, setup

setup(
    name="yall",
    version="0.1.0",
    author="BroncBotz 3481",
    description="An improved version of the LimelightHelpers script released by LimelightVision.",
    packages=find_packages(),
    install_requires=[
        "robotpy-wpimath",
        "wpilib",
        "pyntcore",
    ],
    python_requires=">=3.8",
)
